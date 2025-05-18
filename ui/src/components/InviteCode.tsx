'use client';

import { useState } from 'react';
import { FiCopy, FiCheck } from 'react-icons/fi';

interface InviteCodeProps {
  port: number | null;
}

export default function InviteCode({ port }: InviteCodeProps) {
  const [copied, setCopied] = useState(false);
  
  if (!port) return null;
  
  const copyToClipboard = () => {
    navigator.clipboard.writeText(port.toString());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  
  return (
    <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg">
      <h3 className="text-lg font-medium text-green-800">File Ready to Share!</h3>
      <p className="text-sm text-green-600 mb-3">
        Share this invite code with anyone you want to share the file with:
      </p>
      
      <div className="flex items-center">
        <div className="flex-1 bg-white p-3 rounded-l-md border border-r-0 border-gray-300 font-mono text-lg">
          {port}
        </div>
        <button
          onClick={copyToClipboard}
          className="p-3 bg-blue-500 hover:bg-blue-600 text-white rounded-r-md transition-colors"
          aria-label="Copy invite code"
        >
          {copied ? <FiCheck className="w-5 h-5" /> : <FiCopy className="w-5 h-5" />}
        </button>
      </div>
      
      <p className="mt-3 text-xs text-gray-500">
        This code will be valid as long as your file sharing session is active.
      </p>
    </div>
  );
}
