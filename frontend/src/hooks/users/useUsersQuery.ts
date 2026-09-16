import { useQuery } from '@tanstack/react-query'
import { getUsers } from '@/api/users/list'

export function useUsersQuery() {
  return useQuery({
    queryKey: ['users'],
    queryFn: () => getUsers(),
    staleTime: 5 * 60 * 1000,
    
  })
}
