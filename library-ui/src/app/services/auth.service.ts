import {Injectable, signal} from '@angular/core';
import {Observable, of} from 'rxjs';
import {catchError, switchMap, tap} from 'rxjs/operators';
import {ApiService} from './api.service';
import {UserRegisterRequest} from '../interfaces/user-register-request';
import {UserResponse, UserRole} from '../interfaces/user-response';
import {AuthenticationRequest} from '../interfaces/authentication-request';
import {UpdateProfileRequest} from '../interfaces/update-profile-request';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  currentUser = signal<UserResponse | null>(null);

  constructor(private apiService: ApiService) {
  }

  login(credentials: AuthenticationRequest): Observable<UserResponse> {
    return this.apiService.post<UserResponse>('/auth/authenticate', {
      body: credentials
    }).pipe(
      tap(response => {
        this.currentUser.set(response);
      })
    );
  }

  register(request: UserRegisterRequest): Observable<UserResponse> {
    return this.apiService.post<UserResponse>('/auth/register', {
      body: request
    });
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserResponse> {
    return this.apiService.patch<UserResponse>('/users/me', {
      body: request
    }).pipe(
      tap(response => this.currentUser.set(response))
    );
  }

  private getCurrentUser(): Observable<UserResponse> {
    return this.apiService.get<UserResponse>('/users/me', {});
  }

  logout(): Observable<void> {
    return this.apiService.post<void>('/auth/logout', {}).pipe(
      tap(() => {
        this.logoutLocally();
      })
    );
  }

  refreshToken(): Observable<void> {
    return this.apiService.post<void>('/auth/refresh', {});
  }

  logoutLocally(): void {
    this.currentUser.set(null);
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === UserRole.ADMIN;
  }

  isUser(): boolean {
    return this.currentUser()?.role === UserRole.USER;
  }

  isAuthenticated(): Observable<boolean> {
    if (!!this.currentUser()) {
      return of(true);
    }

    return this.getCurrentUser().pipe(
      switchMap((response) => {
        this.currentUser.set(response);
        return of(true);
      }),
      catchError(() => of(false))
    );
  }

}
