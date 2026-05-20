import {Author} from './author';
import {Category} from './category';
import {LibraryBookStatus} from './library-book';

export interface Range<T> {
  min: T | null;
  max: T | null;
}

export interface BaseBookFilters {
  title: string;
  author: Author | null;
  category: Category | null;
  publishYear: Range<number>;
  pages: Range<number>;
  languages: string[];
}

export interface LibraryFilters extends BaseBookFilters {
  status: LibraryBookStatus | null;
  rating: Range<number>;
  location: string;
}

export interface AuthorFilters {
  name: string;
  country: string | null;
  birthYear: Range<number>;
  booksCount: Range<number>;
}

export interface CategoryFilters {
  name: string;
  booksCount: Range<number>;
}
