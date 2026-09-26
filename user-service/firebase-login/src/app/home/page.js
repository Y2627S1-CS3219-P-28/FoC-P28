'use client'

import { useEffect, useState } from 'react';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { auth } from '@/app/firebase/config';
import { useRouter } from 'next/navigation'

const home = () => {
  const router = useRouter();

  // Create user state, setUser function and initial user to null
  const [user, setUser] = useState(null);

  const logOut = async () => {
    try {
      // Firebase Client Sign Out
      await signOut(auth);
      setUser(null);
      
      // Redirect the user to the login page
      router.push('/login');
    } catch (error) {
      console.error('Error logging out:', error);
    }
  };

  const profile = async () => {
    try {
      // Authenticate user before opening?
      //...
      // Redirect the user to the profile page
      router.push('./profile');
    } catch (error) {
      console.error('Error accessing profile:', error);
    }
  }

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      if (!currentUser) {
        router.push('/login');
      } else {
        setUser(currentUser);
      }
    });

    return () => unsubscribe();
  }, []);

  if (!user) {
    return <p>Loading...</p>;
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center px-10">
      <div>
        <h1>User Dashboard</h1>
        <p>Welcome, {user.email}</p>
        <div>
          <br></br>
            <button onClick = {profile}
              className="w-100 p-3 bg-indigo-600 
              rounded text-white hover:bg-indigo-500">
              View profile
            </button>
        </div>
        <div>
          <br></br>
            <button onClick = {logOut}
              className="w-100 p-3 bg-indigo-600 
              rounded text-white hover:bg-indigo-500">
              Log Out
            </button>
        </div>
      </div>
    </main>
  );
};

export default home;