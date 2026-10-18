// src/mutations/useUpdateUser.ts
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateUser } from '@/api/users/mutations'
import type { UserList } from '@/api/users'

export function useUpdateUserMutation(id: string, mergedData: UserList) { //TODO: remove mergedData when API is implemented
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: Partial<UserList>) => updateUser(id, data, mergedData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', id] })
      queryClient.invalidateQueries({ queryKey: ['users'] }) // list page
    },
  }) //mutate sites and site too
}
