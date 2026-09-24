// import the dependencies required for Express.js
import express from 'express'
// import the dependencies required for cors
import cors from 'cors'
// import the dependencies required for dotenv
// this automatically reads and loads the .env file
import 'dotenv/config'
// import the connectDB function created earlier
import connectDB from './config/db.js'
import userRoutes from './routes/userRoutes.js'

// initialize the Express.js application
// store it in the app variable
const app = express()

// allow cross-origin requests to reach the Express.js server
// from localhost:3000, which is your frontend domain
app.options(
  /.*/,
  cors({
    origin: 'http://localhost:3000',
    optionsSuccessStatus: 200,
  }),
)
app.use(cors())

// allow JSON data in request body to be parsed
app.use(express.json())
// allow URL-encoded data in request body to be parsed
app.use(express.urlencoded({ extended: false }))

// initialize connection to MongoDB database
connectDB()

/**
 * code to initialize the Express.js application store it in the app variable ... (same as before)
 * code to allow cross-origin requests to reach the Express.js server ... (same as before)
**/

// use 8080 as a fallback if PORT is undefined in .env file
const PORT = process.env.PORT || 8080


// configure the Express.js application to run at port 8080
// since you will be running this application on your computer (localhost),
// the backend server will be running at http://localhost:8080
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}...`)
})

// use the user router to handle requests
// at http://localhost:8080/api/users
app.use('/api/users', userRoutes)

// when a GET request is made to http://localhost:8080/,
// the response will be { message: 'Hello World' } in JSON format
app.get('/', (req, res) => {
  res.json({ message: 'Hello World' })
})

// export Express.js application to be used elsewhere
export default app