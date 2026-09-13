package com.example.whataday.collector;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 基于 JNA 的 Windows 前台窗口读取。
 *
 * <p>两步：先用 {@code user32} 拿到前台窗口句柄与标题，再用窗口所属进程 id 反查进程名。
 * 判空与异常都收敛在这里，向上只暴露「有没有读到」，调用方不需要关心 Win32 的错误码。
 *
 * <p>注意：本类只在 {@code mode=desktop} 时装配，默认的 mock 模式完全不会加载它，
 * 因此 Linux 上也能正常构建与测试。
 */
@Component
@ConditionalOnProperty(name = "whataday.collector.mode", havingValue = "desktop")
public class WindowsActiveWindowReader implements ActiveWindowReader {

    private static final Logger log = LoggerFactory.getLogger(WindowsActiveWindowReader.class);

    /** PROCESS_QUERY_LIMITED_INFORMATION：查询进程信息所需的最小权限。 */
    private static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;

    private static final int MAX_PATH_CHARS = 1024;

    @Override
    public Optional<WindowInfo> readForegroundWindow() {
        try {
            HWND handle = User32.INSTANCE.GetForegroundWindow();
            if (handle == null) {
                return Optional.empty();
            }
            return Optional.of(new WindowInfo(processNameOf(handle), windowTitleOf(handle)));
        } catch (Throwable t) {
            // 采集是后台行为，任何异常都不应该打断调度循环
            log.debug("读取前台窗口失败：{}", t.toString());
            return Optional.empty();
        }
    }

    private String windowTitleOf(HWND handle) {
        int length = User32.INSTANCE.GetWindowTextLength(handle);
        if (length <= 0) {
            return "";
        }
        char[] buffer = new char[length + 1];
        User32.INSTANCE.GetWindowText(handle, buffer, buffer.length);
        return Native.toString(buffer).trim();
    }

    private String processNameOf(HWND handle) {
        IntByReference pidRef = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(handle, pidRef);
        int pid = pidRef.getValue();
        if (pid == 0) {
            return null;
        }

        HANDLE process = Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
        if (process == null) {
            return null;
        }
        try {
            char[] path = new char[MAX_PATH_CHARS];
            IntByReference size = new IntByReference(path.length);
            if (!Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, path, size)) {
                return null;
            }
            String fullPath = Native.toString(path);
            int separator = fullPath.lastIndexOf('\\');
            return separator >= 0 ? fullPath.substring(separator + 1) : fullPath;
        } finally {
            Kernel32.INSTANCE.CloseHandle(process);
        }
    }
}
