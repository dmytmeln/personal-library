import {UserResponse} from './user-response';

export interface UserUpdateResponse {
  user: UserResponse;
  accessToken?: string;
  refreshToken?: string;
}
