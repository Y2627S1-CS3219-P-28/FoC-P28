// app/login/page.js
'use client'
import React from "react";
import { useRef } from "react"
import { auth } from '@/app/firebase/config';
import {
    signInWithEmailAndPassword
} from "firebase/auth";
import { useRouter } from 'next/navigation'

const login = () => {
    const router = useRouter();

    const logemailRef = useRef();
    const logpasswordRef = useRef();

    const logIn = (e) => {
        e.preventDefault();

        const email = logemailRef.current.value;
        const password = logpasswordRef.current.value;

        signInWithEmailAndPassword(auth, email, password)
            .then((userCredential) => {
                // Signed in 
                const user = userCredential.user;
                console.log(user)

                if (user && user.emailVerified) {
                    console.log(`${user.email} is verified`)
                    alert(`Welcome ${user.email}, redirecting to home page.`)
                    //router to next page
                    router.push('/home');
                } else {
                    console.log(`${user.email} is not verified`)
                    alert(`${user.email}, please verify your email first.`)
                }
                

            })
            .catch((error) => {
                const errorCode = error.code;
                const errorMessage = error.message;
                alert(errorMessage)
            });
    }

    function signUp() {
        router.push('/signup');
    }

    return (
        <div>
            <center>
                <h1>Log in screen</h1><br /><br />
                <form onSubmit={logIn}>
                    <input type="email"
                        placeholder="Enter your email"
                        ref={logemailRef}
                        style={{ color: 'white' }} /><br />
                    <br></br>
                    <input type="password"
                        placeholder="Enter your password"
                        ref={logpasswordRef}
                        style={{ color: 'white' }} /><br />
                    <br /><button type="submit"
                        className="w-200 p-3 bg-indigo-600 
        rounded text-white hover:bg-indigo-500">
                        Log In
                    </button>
                </form>
                <div>
                    <br></br>
                    <button onClick = {signUp}
                        className="w-200 p-3 bg-indigo-600 
                        rounded text-white hover:bg-indigo-500">
                        Sign Up
                    </button>
                </div>
            </center>
        </div>
    )
}
export default login