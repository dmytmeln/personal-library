import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {BehaviorSubject, Observable, throwError} from 'rxjs';
import {catchError, filter, switchMap, take} from 'rxjs/operators';
import {AuthService} from './auth.service';
import {Router} from '@angular/router';

type RefreshState = 'WAITING' | 'READY';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  private isRefreshing = false;
  private refreshStateSubject = new BehaviorSubject<RefreshState>('READY');

  constructor(private authService: AuthService, private router: Router) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error) => {
        if (this.isUnauthorizedError(error, req)) {
          return this.handle401Error(req, next);
        }
        return throwError(() => error);
      })
    );
  }

  private isUnauthorizedError(error: any, req: HttpRequest<any>): boolean {
    return this.is401Error(error) && !this.isAuthEndpoint(req.url);
  }

  private is401Error(error: any): boolean {
    return error instanceof HttpErrorResponse && error.status === 401;
  }

  private isAuthEndpoint(url: string): boolean {
    return url.includes('/auth/authenticate') || url.includes('/auth/refresh');
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (this.isRefreshing) {
      return this.enqueueRequestUntilTokenRefreshed(request, next);
    }
    return this.refreshAccessTokenAndRetry(request, next);
  }

  private enqueueRequestUntilTokenRefreshed(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return this.refreshStateSubject.pipe(
      filter(state => state === 'READY'),
      take(1),
      switchMap(() => next.handle(request))
    );
  }

  private refreshAccessTokenAndRetry(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const skipRedirect = request.headers.get('X-Skip-Auth-Redirect') === 'true';

    this.isRefreshing = true;
    this.refreshStateSubject.next('WAITING');

    return this.authService.refreshToken().pipe(
      switchMap(() => {
        this.isRefreshing = false;
        this.refreshStateSubject.next('READY');
        return next.handle(request);
      }),
      catchError((err) => {
        this.isRefreshing = false;
        this.refreshStateSubject.next('READY');
        this.authService.logoutLocally();
        if (!skipRedirect) {
          this.router.navigate(['/login']);
        }
        return throwError(() => err);
      })
    );
  }

}
