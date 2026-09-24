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

    const emailRef = useRef();
    const passwordRef = useRef();

    const signup = async (e) => {
        e.preventDefault();

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
        
            console.log("Verification email sent!");
                    
            const response = await fetch('http://localhost:8080/api/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: user.email
                })
            });

            // Checks if API succeeded
            if (!response.ok) {
                const text = await response.text();
                console.log("API response:", text);
                
                const data = await response.json();
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

        <div>
            <center>
                <h1>Sign Up screen</h1><br /><br />
                <form onSubmit={signup}>
                    <input type="email"
                        placeholder="Enter your email"
                        ref={emailRef}
                        style={{ color: 'white' }} />
                    <br /><br></br>
                    <input type="password"
                        placeholder="Enter your password"
                        ref={passwordRef}
                        style={{ color: 'white' }} /><br />
                    <br />
                    <button type="submit"
                        className="w-200 p-3 bg-indigo-600 
                     rounded text-white hover:bg-indigo-500">
                        Sign Up
                    </button>
                </form>
            </center>
        </div>
    )
}

export default signup