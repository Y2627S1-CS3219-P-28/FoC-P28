"use client"

import { useState } from 'react';
import { useRouter } from 'next/navigation';

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"
import { Button } from "@/components/ui/button"

interface DeleteAccountButtonProps {
    onError: (message: string) => void;
}

export default function DeleteAccountButton({
      onError,
    }: DeleteAccountButtonProps) {
  const { user, getIdToken, signOut } = useAuth();
  const config = useConfig();
  const router = useRouter();

  const [deleting, setDeleting] = useState(false);

  async function handleDelete() {
    const confirmed = window.confirm(
      'Are you sure you want to permanently delete your account? This action cannot be undone.'
    );

    if (!confirmed) return;

    setDeleting(true);

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
        let message = 'Failed to delete account. Please try again.';

        try {
          const body = await response.json();

          message = body.detail ?? body.message ?? message;         
        } catch {
          // Keep the generic fallback if the error body cannot be parsed.
        }

        console.log("Setting error to:", message);
        onError(message);
        return
      }

      // Sign out locally after deletion is done
      await signOut();
      router.replace('/login');
    } catch (err) {
      const message =
        err instanceof Error
          ? err.message
          : "Unexpected error occurred.";
      
      console.log("Catch block setting error:", message);
      onError(message);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div>
      <Button
        type="button"
        variant="destructive"
        className="w-fit"
        onClick={handleDelete}
        disabled={deleting}
      >
        {deleting ? 'Deleting account...' : 'Delete account'}
      </Button>
    </div>
  );
}

export { DeleteAccountButton }