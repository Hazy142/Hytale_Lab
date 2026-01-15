# Hytale Network Protocol Specification v1.0

## 1. Transport Layer
- **Protocol:** QUIC over UDP
- **Port:** 5520
- **Connection Type:** Persistent, multiplexed streams
- **Encryption:** TLS 1.3 (handled by QUIC layer)

## 2. Framing & Serialization
- **Packet Format:** `[VarInt: PacketID] [Payload]`
- **VarInt Encoding:** LEB128 (Little Endian Base 128), up to 5 bytes.
- **Compression:** ZStandard or GZIP (optional, signaled via handshake or compression flag in header).
- **Endianness:** Big-endian (Network Byte Order) for primitive types inside the payload.
- **String Encoding:** `[VarInt: Length] [Bytes: UTF-8 String]`

## 3. Packet Categories

### 3.1 Client → Server (Serverbound)
- **0x01:** Movement Input (Position, Rotation, Flags)
- **0x03:** Chat Message (Text content)
- **0x05:** Block Interaction (Placement, Breaking)
- **0x07:** Item Use (Right-click item)
- **0x09:** Player Action (Sneak, Sprint, Bed enter)

### 3.2 Server → Client (Clientbound)
- **0x02:** Movement Update (Relative/Absolute Entity Move)
- **0x04:** Chat Broadcast (Chat message to UI)
- **0x06:** Block Update (Single block change)
- **0x08:** Entity Spawn (Create new entity in world)
- **0x0A:** Game Phase Change (Lobby -> Day -> Night)

## 4. Authentication & Handshake
1.  **Handshake (0x00):** Client sends protocol version and intended server address.
2.  **Auth Request:** Server challenges client with encryption token.
3.  **Auth Response:** Client signs token (Mojang/Riot auth) and returns it.
4.  **Login Success:** Server confirms UUID and compression threshold.

## 5. Rate Limits & Flow Control
- **Movement Packets:** Max 20/sec (sent every tick/50ms). Flood protection enables "rubber-banding".
- **Chat Packets:** Max 5/sec (spam protection).
- **Keep-Alive:** Bidirectional ping every 30 seconds. Timeout after 60 seconds.
