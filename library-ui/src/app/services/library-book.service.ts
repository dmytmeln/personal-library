import {Injectable} from '@angular/core';
import {ApiService} from './api.service';
import {Observable} from 'rxjs';
import {LibraryBook, LibraryBookStatus} from '../interfaces/library-book';
import {Page} from '../interfaces/page';
import {UpdateLibraryBookDetails} from '../interfaces/update-library-book-details';
import {BulkLibraryBookRequest} from '../interfaces/bulk-library-book-request';
import {LanguageWithCount} from '../interfaces/language-with-count';
import {CreateLocalBook} from '../interfaces/create-local-book';

export interface LibraryBookQueryOptions {
  page?: number;
  size?: number;
  sort?: string[];
  title?: string;
  status?: LibraryBookStatus | null;
  location?: string;
  authorId?: number;
  categoryId?: number;
  publishYearMin?: number;
  publishYearMax?: number;
  pagesMin?: number;
  pagesMax?: number;
  ratingMin?: number;
  ratingMax?: number;
  languages?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class LibraryBookService {

  constructor(
    private apiService: ApiService,
  ) {
  }

  getAll(options: LibraryBookQueryOptions = {}): Observable<Page<LibraryBook>> {
    return this.apiService.get('/users/me/library-books', {params: this.buildParams(options)});
  }

  getLanguages(): Observable<LanguageWithCount[]> {
    return this.apiService.get('/users/me/library-books/languages', {});
  }

  addBook(bookId: number): Observable<LibraryBook> {
    return this.apiService.post('/users/me/library-books', {params: {bookId}});
  }

  createLocalBook(dto: CreateLocalBook): Observable<void> {
    return this.apiService.post('/users/me/library-books/local', {body: dto});
  }

  updateLocalBook(libraryBookId: number, dto: CreateLocalBook): Observable<void> {
    return this.apiService.put(`/users/me/library-books/local/${libraryBookId}`, {body: dto});
  }

  bulkAdd(ids: number[]): Observable<void> {
    const body: BulkLibraryBookRequest = {ids};
    return this.apiService.post('/users/me/library-books/bulk', {body});
  }

  removeBook(libraryBookId: number): Observable<void> {
    return this.apiService.delete(`/users/me/library-books/${libraryBookId}`, {});
  }

  bulkRemove(ids: number[]): Observable<void> {
    const body: BulkLibraryBookRequest = {ids};
    return this.apiService.post('/users/me/library-books/bulk-remove', {body});
  }

  bulkUpdateStatus(ids: number[], status: LibraryBookStatus): Observable<void> {
    const body = {ids, status};
    return this.apiService.put('/users/me/library-books/bulk-status', {body});
  }

  changeStatus(libraryBookId: number, status: LibraryBookStatus): Observable<LibraryBook> {
    return this.apiService.put(`/users/me/library-books/${libraryBookId}/status`, {params: {status}});
  }

  changeRating(libraryBookId: number, rating: number): Observable<LibraryBook> {
    return this.apiService.put(`/users/me/library-books/${libraryBookId}/rating`, {params: {rating}});
  }

  updateDetails(libraryBookId: number, dto: Partial<UpdateLibraryBookDetails>): Observable<LibraryBook> {
    return this.apiService.put(`/users/me/library-books/${libraryBookId}/details`, {body: dto});
  }

  updateLocation(libraryBookId: number, location: string | null): Observable<LibraryBook> {
    return this.apiService.put(`/users/me/library-books/${libraryBookId}/location`, {body: {location}});
  }

  resetDetails(libraryBookId: number): Observable<LibraryBook> {
    return this.apiService.put(`/users/me/library-books/${libraryBookId}/details/reset`, {});
  }

  searchByMood(query: string, status?: LibraryBookStatus | null, limit?: number): Observable<LibraryBook[]> {
    const params: any = {query};
    if (status) params.status = status;
    if (limit) params.limit = limit;
    return this.apiService.get('/users/me/library-books/search-by-mood', {params});
  }

  private buildParams(options: LibraryBookQueryOptions): LibraryBookQueryOptions {
    const {
      page = 0,
      size = 15,
      sort,
      title,
      status,
      location,
      authorId,
      categoryId,
      publishYearMin,
      publishYearMax,
      pagesMin,
      pagesMax,
      ratingMin,
      ratingMax,
      languages
    } = options;

    const params: LibraryBookQueryOptions = {page, size};

    if (sort && sort.length > 0) params.sort = sort;
    if (title) params.title = title;
    if (status) params.status = status;
    if (location) params.location = location;
    if (authorId) params.authorId = authorId;
    if (categoryId) params.categoryId = categoryId;
    if (publishYearMin != null) params.publishYearMin = publishYearMin;
    if (publishYearMax != null) params.publishYearMax = publishYearMax;
    if (pagesMin != null) params.pagesMin = pagesMin;
    if (pagesMax != null) params.pagesMax = pagesMax;
    if (ratingMin != null) params.ratingMin = ratingMin;
    if (ratingMax != null) params.ratingMax = ratingMax;
    if (languages && languages.length > 0) params.languages = languages;

    return params;
  }

}
