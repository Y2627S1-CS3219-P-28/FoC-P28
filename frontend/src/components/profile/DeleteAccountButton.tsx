"use client"

import { useState } from 'react';
import { useRouter } from 'next/navigation';

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"
import { Button } from "@/components/ui/button"

export default function DeleteAccountButton() {
  const { user, getIdToken, signOut } = useAuth();
  const config = useConfig();
  const router = useRouter();

  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  async function handleDelete() {
    const confirmed = window.confirm(
      'Are you sure you want to permanently delete your account? This action cannot be undone.'
    );

    if (!confirmed) return;

    setDeleting(true);
    setError('');

    try {
      if (!user) {
        throw new Error('You must be logged in to delete your account.');
      }

      const token = await getIdToken();

      const response = await fetch(
        `${config.apiBaseUrl}/api/users/me`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to delete account. Please try again.');
      }

      // Sign out locally after deletion is done
      await signOut();
      router.replace('/login');
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Unexpected error occurred.'
      );
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div>
      <Button
        type="button"
        variant="destructive"
        onClick={handleDelete}
        disabled={deleting}
      >
        {deleting ? 'Deleting account...' : 'Delete account'}
      </Button>

      {error && <p role="alert">{error}</p>}
    </div>
  );
}

export { DeleteAccountButton }