import {Injectable} from '@angular/core';
import {ApiService} from './api.service';
import {Observable} from 'rxjs';
import {Book} from '../interfaces/book';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {

  private readonly baseUrl = '/recommendations';

  constructor(private apiService: ApiService) {
  }

  getPersonalized(limit?: number): Observable<Book[]> {
    const params: any = {};
    if (limit) params.limit = limit;
    return this.apiService.get<Book[]>(this.baseUrl, {params});
  }

  getPopular(limit?: number): Observable<Book[]> {
    const params: any = {};
    if (limit) params.limit = limit;
    return this.apiService.get<Book[]>(`${this.baseUrl}/popular`, {params});
  }

  getNewArrivals(limit?: number): Observable<Book[]> {
    const params: any = {};
    if (limit) params.limit = limit;
    return this.apiService.get<Book[]>(`${this.baseUrl}/new`, {params});
  }

  getTrendingInFavoriteGenres(limit?: number): Observable<Book[]> {
    const params: any = {};
    if (limit) params.limit = limit;
    return this.apiService.get<Book[]>(`${this.baseUrl}/trending-genres`, {params});
  }

  getSimilar(bookId: number, limit?: number): Observable<Book[]> {
    const params: any = {limit};
    if (!limit) delete params.limit;
    return this.apiService.get<Book[]>(`${this.baseUrl}/similar/${bookId}`, {params});
  }

  searchByMood(query: string, limit?: number): Observable<Book[]> {
    const params: any = {query};
    if (limit) params.limit = limit;
    return this.apiService.get<Book[]>(`${this.baseUrl}/search-by-mood`, {params});
  }

}
