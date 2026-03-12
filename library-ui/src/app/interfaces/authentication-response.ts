import {UserResponse} from './user-response';

export interface AuthenticationResponse {
  accessToken: string;
  refreshToken: string;
  userResponse: UserResponse;
}
