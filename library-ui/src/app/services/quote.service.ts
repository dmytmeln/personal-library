import {Injectable} from '@angular/core';
import {ApiService} from './api.service';
import {Observable} from 'rxjs';
import {Quote, QuoteRequest} from '../interfaces/quote';

@Injectable({
  providedIn: 'root'
})
export class QuoteService {

  private readonly API_URL = '/quotes';

  constructor(private api: ApiService) { }

  getByLibraryBookId(libraryBookId: number): Observable<Quote[]> {
    return this.api.get(this.API_URL, { params: { libraryBookId } });
  }

  create(libraryBookId: number, request: QuoteRequest): Observable<Quote> {
    return this.api.post(`${this.API_URL}/${libraryBookId}`, { body: request });
  }

  update(quoteId: number, request: QuoteRequest): Observable<Quote> {
    return this.api.put(`${this.API_URL}/${quoteId}`, { body: request });
  }

  delete(quoteId: number): Observable<void> {
    return this.api.delete(`${this.API_URL}/${quoteId}`, {});
  }

}
