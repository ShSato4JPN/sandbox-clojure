import { ApolloClient, InMemoryCache, HttpLink } from '@apollo/client'

// uri が相対パスなので、Vite の proxy 経由で :8888 の /api に届く。
export const client = new ApolloClient({
  link: new HttpLink({ uri: '/api' }),
  cache: new InMemoryCache(),
})
