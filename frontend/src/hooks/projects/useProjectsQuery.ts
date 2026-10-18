import { useQuery } from '@tanstack/react-query'
import { getProjects } from '@/api/projects'

/** Loads the project list only when its owning permission section is enabled. */
export function useProjectsQuery(enabled = true) {
  return useQuery({
    queryKey: ['projects'],
    queryFn: () => getProjects(),
    staleTime: 1 * 60 * 1000,
    enabled,
  })
}
