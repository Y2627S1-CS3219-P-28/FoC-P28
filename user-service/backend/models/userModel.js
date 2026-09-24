import mongoose from 'mongoose'

const userSchema = mongoose.Schema({
  email: {
    type: String,
    required: [true, 'Please enter user email'],
  },
  roles: {
    type: [String],
    required: [true, 'Please enter user roles'],
    default: ["requester", "courier"]
  },
  penalty: {
    type: Number,
    required: [true, 'Please enter penalty score'],
    default: 0
  },
})

// export Address model to be used in controller
export default mongoose.model('User', userSchema)