// import User model
import User from '../models/userModel.js'

// @desc    Fetch all users
// @route   GET /api/users
// @access  Public
const fetchAllUsers = async (req, res) => {
  // function provided by Mongoose to fetch all Address documents
  const users = await User.find({})

  // return all addresses in JSON format
  // with success status 200
  res.status(200).json(users)
}


// @desc    Add a user
// @route   POST /api/users
// @access  Public
const addUser = async (req, res) => {
  const { email } = req.body

  // validate request body
  if (!email) {
    return res.status(400).json({ message: 'Please enter all fields.' })
  }

  try {
    // function provided by Mongoose to create a new Address document
    const user = await User.create({
      email,
    })

    // return the newly created Address in JSON format
    // with created success status 201
    res.status(201).json({
      _id: user._id,
      email: user.email,
      roles: user.roles,
      penalty: user.penalty
    })
  } catch (error) {
    // catch exception when fields are missing
    res.status(400).json({ message: 'Invalid user data.' })
  }
}


// @desc    Update a user
// @route   PUT /api/users
// @access  Public
const updateUser = async (req, res) => {
  const { email, roles } = req.body
  const penalty = req.body.penalty || null

  // validate request body
  if (!email || !roles) {
    return res.status(400).json({ message: 'Please enter all fields.' })
  }

  try {
    // function provided by mongoose to find an
    // Address document with a given ID
    // req.params.id is retrieved from /:id in route
    const user = await User.findById(req.params.id)

    // update the document
    user.email = email
    user.roles = roles

    // only update penalty score if it is specified
    if (penalty != null) {
        user.penalty = penalty
    }

    // function provided by mongoose to
    // save the changes made to a document
    await user.save()

    // return the updated address in JSON format
    // with success status 200
    res.status(200).json({
      _id: user._id,
      email: user.email,
      roles: user.roles,
      penalty: user.penalty
    })
  } catch (error) {
    res.status(400).json({ message: 'Invalid address data.' })
  }
}


// @desc    Delete a user
// @route   DELETE /api/users
// @access  Public
const deleteUser = async (req, res) => {
  try {
    // function provided by mongoose to find a
    // User document with a given ID
    // req.params.id is retrieved from /:id in route
    const user = await User.findById(req.params.id)

    // function provided by mongoose to delete a document
    await user.deleteOne()


    res.status(200).json({ message: 'User removed' })
  } catch (error) {
    res.status(404).json({ message: 'User not found' })
  }
}

// export controller functions to be used in corresponding route
export { fetchAllUsers, addUser, updateUser, deleteUser }