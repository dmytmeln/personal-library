import {Book} from './book';

export interface LibraryBook {
  id: number;
  status: LibraryBookStatus,
  addedAt: Date,
  rating?: number,
  location?: string | null,
  book: Book
}

export enum LibraryBookStatus {
  FAVORITE = 'FAVORITE',
  TO_READ = 'TO_READ',
  READ = 'READ',
  READING = 'READING',
  STOP = 'STOP',
  NO_TAG = 'NO_TAG',
  WISHLIST = 'WISHLIST',
}

export const LIBRARY_BOOK_STATUSES = [
  LibraryBookStatus.FAVORITE,
  LibraryBookStatus.TO_READ,
  LibraryBookStatus.READ,
  LibraryBookStatus.READING,
  LibraryBookStatus.STOP,
  LibraryBookStatus.NO_TAG,
  LibraryBookStatus.WISHLIST,
];
