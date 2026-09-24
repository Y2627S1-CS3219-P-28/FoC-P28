import express from 'express'
const router = express.Router()

import { fetchAllUsers, addUser, updateUser, deleteUser } from '../controllers/userController.js'

// call the fetchAllUsers function
// when a GET request is made to http://localhost:8080/api/users/
router.route('/').get(fetchAllUsers)

// call the addUser function
// when a POST request is made to http://localhost:8080/api/users/
router.route('/').post(addUser)

// call the updateUser function
// when a PUT request is made to http://localhost:8080/api/users/:id
router.route('/:id').put(updateUser)

// call the deleteUser function
// when a DELETE request is made to http://localhost:8080/api/users/:id
router.route('/:id').delete(deleteUser)

export default router