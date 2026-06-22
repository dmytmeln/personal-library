export interface Quote {
  id: number;
  libraryBookId: number;
  text: string;
  page?: string;
  comment?: string;
  createdAt: string;
}

export interface QuoteRequest {
  text: string;
  page?: string;
  comment?: string;
}
