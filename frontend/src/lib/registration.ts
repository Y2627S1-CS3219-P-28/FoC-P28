export type RegistrationFactPayload = {
  eventId: string
  userId: string
  occurredAt: string
}

export function buildRegistrationFactPayload(
  userId: string,
  eventId: string,
  occurredAt: string,
): RegistrationFactPayload {
  return { eventId, userId, occurredAt }
}
