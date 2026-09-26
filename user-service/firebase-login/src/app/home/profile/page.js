'use client'

import { useEffect, useState } from 'react';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { auth } from '@/app/firebase/config';
import { useRouter } from 'next/navigation'

const profile = () => {
  const router = useRouter();

  // Create user state, setUser function and initial user to null
  const [user, setUser] = useState(null);
  // Create profile state, setProfile and initial profile to null
  const [profile, setProfile] = useState(null);

  const home = async () => {
    try {
      // Authenticate user before opening?
      //...
      // Redirect the user to the home page
      router.push('..');
    } catch (error) {
      console.error('Error accessing profile:', error);
    }
  }

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (!currentUser) {
        router.push('/login');
      } else {
        setUser(currentUser);
      }

      try {
        // Get Firebase ID token
        // const idToken = await currentUser.getIdToken();
        const userId = currentUser.uid;

        const response = await fetch('http://localhost:8080/api/users/me', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            // Authorization: `Bearer ${idToken}`
            userId: userId
          })
        });

        if (!response.ok) {
          throw new Error('Failed to retrieve user profile');
        }
        const data = await response.json();

        setProfile(data);
      } catch (error) {
        console.error(error.message);
      }
    });

    return () => unsubscribe();
  }, []);

  if (!user) {
    return <p>Loading...</p>;
  }

  if (!profile) {
    return <p>Profile not found.</p>;
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center px-10">
      <div>
        <h1>User Profile</h1>
          <div>
            <p className="text-gray-400">Username: </p>
            <p>{profile.username}</p>
          </div>
          <div>
            <p className="text-gray-400">Email: </p>
            <p>{profile.email}</p>
          </div>
          <div>
            <p className="text-gray-400">Roles: </p>
            <p>{profile.roles.join(', ')}</p>
          </div>
          <div>
            <p className="text-gray-400">Penalty: </p>
            <p>{profile.penalty}</p>
          </div>
          <div>
            <p className="text-gray-400">Suspended from courier activities: </p>
            <p>{profile.courierSuspension}</p>
          </div>
          <div>
            <p className="text-gray-400">Suspension end date: </p>
            <p>{profile.suspensionEndDate}</p>
          </div>
        <div>
          <br></br>
            <button onClick = {home}
              className="w-100 p-3 bg-indigo-600 
              rounded text-white hover:bg-indigo-500">
              Go back
            </button>
        </div>
      </div>
    </main>
  );
};

export default profile;