import type { Metadata } from "next"
import { PostRequest } from "@/components/orders/post-request"
export const metadata: Metadata = { title: "Post Request" }
export default function Page() { return <PostRequest /> }
