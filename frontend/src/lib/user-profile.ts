import { reload, type User } from "firebase/auth"

export type UserProfile = {
    email: string
    username: string
    roles: string[]
    penalty: number
    isCourierSuspended: boolean
}

export async function loadUserProfile(
    user: User,
    apiBaseUrl: string
): Promise<UserProfile> {
    // Refresh Firebase's local user state.
    await reload(user)

    // Obtain a fresh ID token.
    const token = await user.getIdToken(true)

    const headers = {
        Authorization: `Bearer ${token}`,
    }

    console.log("[Profile] Fetching profile")

    // Load the profile currently stored in MongoDB.
    const profileResponse = await fetch(
        `${apiBaseUrl}/api/users/me`,
        { headers }
    )

    console.log("[Profile] Profile response:", profileResponse.status)


    if (!profileResponse.ok) {
        throw new Error("Failed to load profile.")
    }

    let profile: UserProfile = await profileResponse.json()

    console.log("[Profile] Profile parsed:", profile)

    const firebaseEmail = user.email?.trim() ?? ""
    const storedEmail = profile.email?.trim() ?? ""

    console.log("[Profile] Email comparison:", {
        firebaseEmail,
        storedEmail,
        emailVerified: user.emailVerified,
    })

    // Synchronize only if Firebase and MongoDB disagree.
    if (user.emailVerified && firebaseEmail &&
        firebaseEmail.toLowerCase() !== storedEmail.toLowerCase()) {
        console.log("[Profile] Starting email sync")

        const syncResponse = await fetch(
            `${apiBaseUrl}/api/users/me/email-sync`,
            {
                method: "POST",
                headers,
            }
        )

        console.log("[Profile] Email sync response:", syncResponse.status)

        if (syncResponse.ok) {
            // Use the profile returned by the backend.
            profile = await syncResponse.json()
        } else {
            // Keep the existing profile available even if syncing fails.
            const message = await syncResponse.text()

            console.warn(
                "Email synchronization was not completed:",
                message
            )
        }
    }

    return profile
}