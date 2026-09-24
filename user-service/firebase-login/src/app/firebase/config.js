// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getAuth } from 'firebase/auth'
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyBEGMjbEkDoFj3K_uerljvTpQg0i8ELCPE",
  authDomain: "cs3219-p28-auth.firebaseapp.com",
  projectId: "cs3219-p28-auth",
  storageBucket: "cs3219-p28-auth.firebasestorage.app",
  messagingSenderId: "13819128878",
  appId: "1:13819128878:web:fd3ea6c22c69e213f703bf",
  measurementId: "G-31XZ8M2LML"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
// const analytics = getAnalytics(app);
export const auth = getAuth(app);