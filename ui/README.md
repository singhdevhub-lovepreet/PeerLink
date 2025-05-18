# PeerLink UI

This is the frontend UI for the PeerLink P2P file sharing application. It's built with Next.js, TypeScript, and Tailwind CSS.

## Features

- Drag and drop file upload
- File sharing via invite codes (port numbers)
- File downloading using invite codes
- Modern, responsive UI

## Prerequisites

- Node.js 18+ and npm
- Java 11+ (for the backend)

## Getting Started

### Install Dependencies

```bash
cd ui
npm install
```

### Development Server

```bash
npm run dev
```

This will start the development server on [http://localhost:3000](http://localhost:3000).

### Build for Production

```bash
npm run build
```

### Start Production Server

```bash
npm start
```

## How to Use

1. **Share a File**:
   - Go to the "Share a File" tab
   - Drag and drop a file or click to select one
   - Once uploaded, you'll receive an invite code (port number)
   - Share this invite code with anyone you want to share the file with

2. **Receive a File**:
   - Go to the "Receive a File" tab
   - Enter the invite code you received
   - Click "Download File"
   - The file will be downloaded to your device

## Backend Integration

The UI communicates with the Java backend running on port 8080. Make sure the backend server is running before using the UI.

To start the backend server:

```bash
cd ..  # Go back to the project root
mvn clean package
java -jar target/p2p-1.0-SNAPSHOT.jar
```

## Project Structure

- `src/app`: Next.js app router pages
- `src/components`: React components
- `public`: Static assets
