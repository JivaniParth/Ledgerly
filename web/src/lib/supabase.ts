import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://yfxhjxrnyihawydmmdaz.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmeGhqeHJueWloYXd5ZG1tZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzczNTE3NjgsImV4cCI6MjA5MjkyNzc2OH0.0rIG6rT5mOAsMwE-rhVCV2HsGuPRd5k-EQ-fC8eYcrY';

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
    storage: window.localStorage,
  },
});
