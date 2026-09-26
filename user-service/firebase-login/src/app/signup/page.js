// app/signup/page.js
'use client'
import React from "react";
import { useRef } from 'react'
import { auth } from '@/app/firebase/config';
import {
    createUserWithEmailAndPassword,
    sendEmailVerification 
} from "firebase/auth";
import { useRouter } from 'next/navigation'

const signup = () => {
    const router = useRouter();

    const usernameRef = useRef();
    const emailRef = useRef();
    const passwordRef = useRef();

    const signup = async (e) => {
        e.preventDefault();

        const username = usernameRef.current.value;
        const email = emailRef.current.value;
        const password = passwordRef.current.value;

        try {
            const userCredential = await createUserWithEmailAndPassword(
                auth,
                email, 
                password
            );
            
            // Signed up 
            const user = userCredential.user;
            await sendEmailVerification(auth.currentUser);
        
            console.log("Verification email sent");
                    
            const response = await fetch('http://localhost:8080/api/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: user.uid,
                    username: username,
                    email: user.email
                })
            });

            // Checks if API succeeded
            if (!response.ok) {
                const data = await response.json();

                // Did not go through, delete firebase acc
                await user.delete();

                console.log("API Error: ", data.message || "Unknown error")
                throw new Error(data.message || "Failed to create user");
            }

            alert(`Sign up successful. Please verify your email before logging in.`);

            router.push('/login')
        } catch (error) {
            const errorCode = error.code;
            const errorMessage = error.message;
            
            console.error(error)
            alert(errorMessage);
        }
    };

    return (
        <main className="flex min-h-screen flex-col items-center justify-center px-10">
            <div className="w-full max-w-md text-center">
                <center>
                    <h1>Sign Up</h1><br /><br />
                    <form onSubmit={signup}>
                        <input type="username"
                            placeholder="Enter your username"
                            ref={usernameRef}
                            />
                        <br /><br></br>
                        <input type="email"
                            placeholder="Enter your email"
                            ref={emailRef}
                            />
                        <br /><br></br>
                        <input type="password"
                            placeholder="Enter your password"
                            ref={passwordRef}
                            /><br />
                        <br />
                        <button type="submit"
                            className="w-full p-3 bg-indigo-600 
                            rounded text-white hover:bg-indigo-500">
                            Sign Up
                        </button>
                    </form>
                </center>
            </div>
        </main>
    )
}

export default signup