import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private readonly baseUrl: string = environment.apiBaseUrl + '/api/v1';
  private readonly headers: any = {};

  constructor(
    private httpClient: HttpClient,
  ) {
  }

  get<T>(url: string, data: any): Observable<T> {
    return this.httpClient.get<T>(this.baseUrl + url, this.getRequestOptions(data));
  }

  post<T>(url: string, data: any): Observable<T> {
    return this.httpClient.post<T>(this.baseUrl + url, this.getRequestBody(data), this.getRequestOptions(data));
  }

  put<T>(url: string, data: any): Observable<T> {
    return this.httpClient.put<T>(this.baseUrl + url, this.getRequestBody(data), this.getRequestOptions(data));
  }

  patch<T>(url: string, data: any): Observable<T> {
    return this.httpClient.patch<T>(this.baseUrl + url, this.getRequestBody(data), this.getRequestOptions(data));
  }

  delete<T>(url: string, data: any): Observable<T> {
    return this.httpClient.delete<T>(this.baseUrl + url, this.getRequestOptions(data));
  }

  private getRequestOptions(data: any) {
    let headers = new HttpHeaders(Object.assign({}, this.headers));

    if (data.headers) {
      Object.keys(data.headers).forEach(key => {
        headers = headers.set(key, data.headers[key]);
      });
    }

    return {
      params: data.params ? data.params : {},
      headers,
      withCredentials: true
    };
  }

  private getRequestBody(data: any): any {
    return data.body
      ? data.body
      : {};
  }

}
