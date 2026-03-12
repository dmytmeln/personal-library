export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
}

export const enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER'
}
