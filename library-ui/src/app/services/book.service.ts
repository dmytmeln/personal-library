import {Injectable} from '@angular/core';
import {ApiService} from './api.service';
import {Observable} from 'rxjs';
import {BookDetails} from '../interfaces/book-details';
import {Page} from '../interfaces/page';
import {Book} from '../interfaces/book';
import {LanguageWithCount} from '../interfaces/language-with-count';

export interface BookQueryOptions {
  page?: number,
  size?: number,
  sort?: string[],
  authorId?: number,
  categoryId?: number,
  title?: string,
  publishYearMin?: number,
  publishYearMax?: number,
  languages?: string[],
  pagesMin?: number,
  pagesMax?: number
}

@Injectable({
  providedIn: 'root'
})
export class BookService {

  constructor(private apiService: ApiService) {
  }

  getAll(options: BookQueryOptions = {}): Observable<Page<Book>> {
    const {
      page = 0,
      size = 10,
      sort,
      authorId,
      categoryId,
      title,
      publishYearMin,
      publishYearMax,
      languages,
      pagesMin,
      pagesMax
    } = options;

    let params: BookQueryOptions = {page, size};
    if (sort && sort.length > 0) {
      params.sort = sort;
    }
    if (authorId) {
      params.authorId = authorId;
    }
    if (categoryId) {
      params.categoryId = categoryId;
    }
    if (title) {
      params.title = title;
    }
    if (publishYearMin !== undefined) {
      params.publishYearMin = publishYearMin;
    }
    if (publishYearMax !== undefined) {
      params.publishYearMax = publishYearMax;
    }
    if (languages && languages.length > 0) {
      params.languages = languages;
    }
    if (pagesMin !== undefined) {
      params.pagesMin = pagesMin;
    }
    if (pagesMax !== undefined) {
      params.pagesMax = pagesMax;
    }
    return this.apiService.get('/books', {params});
  }

  getLanguages(): Observable<LanguageWithCount[]> {
    return this.apiService.get('/books/languages', {});
  }

  getBookDetails(bookId: number): Observable<BookDetails> {
    return this.apiService.get(`/books/${bookId}/details`, {});
  }

}
