import { get, post } from './client'
import type { CreateNoteRequest, UserNote } from '@/types'

/** 新增手动记录。 */
export const createNote = (body: CreateNoteRequest) => post<UserNote>('/notes', body)

/** 按日期查询手动记录。 */
export const fetchNotes = (date: string) => get<UserNote[]>('/notes', { date })
