import {Injectable, signal} from '@angular/core';
import {Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {ApiService} from './api.service';
import {UserRegisterRequest} from '../interfaces/user-register-request';
import {UserResponse, UserRole} from '../interfaces/user-response';
import {AuthenticationRequest} from '../interfaces/authentication-request';
import {AuthenticationResponse} from '../interfaces/authentication-response';
import {UpdateProfileRequest} from '../interfaces/update-profile-request';
import {UserUpdateResponse} from '../interfaces/user-update-response';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly ACCESS_TOKEN_KEY = 'auth_access_token';
  private readonly REFRESH_TOKEN_KEY = 'auth_refresh_token';

  currentUser = signal<UserResponse | null>(null);

  constructor(private apiService: ApiService) {
  }

  login(credentials: AuthenticationRequest): Observable<AuthenticationResponse> {
    return this.apiService.post<AuthenticationResponse>('/auth/authenticate', {
      body: credentials
    }).pipe(
      tap(response => {
        localStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
        localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
        this.currentUser.set(response.userResponse);
      })
    );
  }

  register(request: UserRegisterRequest): Observable<UserResponse> {
    return this.apiService.post<UserResponse>('/auth/register', {
      body: request
    });
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserResponse> {
    return this.apiService.patch<UserUpdateResponse>('/users/me', {
      body: request
    }).pipe(
      tap(response => {
        if (response.accessToken) {
          localStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
        }
        if (response.refreshToken) {
          localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
        }
        this.currentUser.set(response.user);
      }),
      map(response => response.user)
    );
  }

  logout(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this.currentUser.set(null);
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === UserRole.ADMIN;
  }

  isUser(): boolean {
    return this.currentUser()?.role === UserRole.USER;
  }

  getToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

}
