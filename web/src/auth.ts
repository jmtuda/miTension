import type { AuthChangeEvent, Session, SupabaseClient } from "@supabase/supabase-js";

export type UserSession = Readonly<{ email: string }>;

export interface AuthGateway {
  getSession(): Promise<UserSession | null>;
  signIn(email: string, password: string): Promise<void>;
  signOut(): Promise<void>;
  onChange(callback: (session: UserSession | null) => void): () => void;
}

export function createSupabaseAuthGateway(client: SupabaseClient): AuthGateway {
  const mapSession = (session: Session | null): UserSession | null =>
    session ? { email: session.user.email ?? "Cuenta autorizada" } : null;

  return {
    async getSession() {
      const { data, error } = await client.auth.getSession();
      if (error) throw new Error(error.message);
      return mapSession(data.session);
    },
    async signIn(email, password) {
      const { error } = await client.auth.signInWithPassword({ email, password });
      if (error) throw new Error(error.message);
    },
    async signOut() {
      const { error } = await client.auth.signOut();
      if (error) throw new Error(error.message);
    },
    onChange(callback) {
      const { data } = client.auth.onAuthStateChange(
        (_event: AuthChangeEvent, session: Session | null) => callback(mapSession(session)),
      );
      return () => data.subscription.unsubscribe();
    },
  };
}
