import { useQuery } from '@tanstack/react-query'
import { getProjects } from '@/api/projects'

export function useProjectsQuery() {
  return useQuery({
    queryKey: ['projects'],
    queryFn: () => getProjects(),
    staleTime: 1 * 60 * 1000,
  })
}
