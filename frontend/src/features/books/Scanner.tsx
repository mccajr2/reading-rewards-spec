import { useRef, useState } from 'react';
import { Button } from '../../components/shared';

interface ScannerProps {
  onResult: (isbnOrUpc: string) => void;
  disabled?: boolean;
}

/**
 * Camera-based barcode scanner using the rear camera.
 * Decodes EAN-13/EAN-8/UPC-A/UPC-E barcodes (book ISBNs and UPCs).
 * Falls back gracefully if the browser has no camera access.
 */
export function Scanner({ onResult, disabled }: ScannerProps) {
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState('');
  const videoRef = useRef<HTMLVideoElement>(null);
  const readerRef = useRef<{ reset: () => void } | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const stopScanner = () => {
    readerRef.current?.reset();
    readerRef.current = null;
    streamRef.current?.getTracks().forEach(t => t.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    setScanning(false);
  };

  const startScanner = async () => {
    setError('');
    const { BrowserMultiFormatReader, BarcodeFormat, DecodeHintType } = await import('@zxing/library');
    const hints = new Map();
    hints.set(DecodeHintType.POSSIBLE_FORMATS, [
      BarcodeFormat.EAN_13,
      BarcodeFormat.EAN_8,
      BarcodeFormat.UPC_A,
      BarcodeFormat.UPC_E,
    ]);

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
      });
      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }

      const reader = new BrowserMultiFormatReader(hints);
      readerRef.current = reader;
      setScanning(true);

      reader.decodeFromVideoDevice(null, videoRef.current!, (result, err) => {
        if (result) {
          const code = result.getText();
          stopScanner();
          onResult(code);
        }
        // Suppress continuous "not found" errors during scanning
        if (err && !(err.message?.includes('No MultiFormat Readers'))) {
          // ignore frame-level not-found errors
        }
      });
    } catch (e: any) {
      setError(e?.message ?? 'Camera access denied or unavailable.');
      stopScanner();
    }
  };

  return (
    <div className="scanner-container">
      {!scanning ? (
        <Button
          type="button"
          variant="secondary"
          className="scanner-btn"
          onClick={startScanner}
          disabled={disabled}
        >
          📷 Scan Barcode
        </Button>
      ) : (
        <Button
          type="button"
          variant="secondary"
          className="scanner-btn"
          onClick={stopScanner}
        >
          ✕ Stop Scanner
        </Button>
      )}
      <video
        ref={videoRef}
        id="scanner-video"
        className={`scanner-video${scanning ? ' scanner-video--active' : ''}`}
        muted
        playsInline
      />
      {error && <p className="error-msg">{error}</p>}
    </div>
  );
}
