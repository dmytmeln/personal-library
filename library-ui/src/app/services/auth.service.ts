import {computed, Injectable, signal} from '@angular/core';
import {Observable, of} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';
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

  isAdmin = computed(() => this.currentUser()?.role === UserRole.ADMIN);
  isUser = computed(() => this.currentUser()?.role === UserRole.USER);

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

  isAuthenticated(): Observable<boolean> {
    if (this.currentUser()) {
      return of(true);
    }

    return this.getCurrentUser().pipe(
      tap(response => this.currentUser.set(response)),
      map(() => true),
      catchError(() => of(false))
    );
  }

  private getCurrentUser(): Observable<UserResponse> {
    return this.apiService.get<UserResponse>('/users/me', {
      headers: {'X-Skip-Auth-Redirect': 'true'}
    });
  }

}
