export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sortBy?: string;
  sortDir?: string;
};

export type PageParams = {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
};
