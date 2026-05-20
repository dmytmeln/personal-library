import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AudioRecorderService {
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];

  readonly isRecording = signal(false);
  readonly duration = signal(0);
  private durationInterval: any;

  readonly MAX_DURATION = 180;
  readonly MIN_DURATION = 5;

  async startRecording(): Promise<void> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      });

      this.audioChunks = [];
      this.mediaRecorder = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus'
      });

      this.mediaRecorder.ondataavailable = (event) => {
        this.audioChunks.push(event.data);
      };

      this.mediaRecorder.start();
      this.isRecording.set(true);
      this.duration.set(0);

      this.durationInterval = setInterval(() => {
        const current = this.duration() + 1;
        this.duration.set(current);

        if (current >= this.MAX_DURATION) {
          this.stopRecording();
        }
      }, 1000);
    } catch (error) {
      console.error('Failed to start recording:', error);
      throw new Error('Microphone access denied');
    }
  }

  stopRecording(): Promise<Blob | null> {
    return new Promise((resolve) => {
      if (!this.mediaRecorder) {
        resolve(null);
        return;
      }

      this.mediaRecorder.onstop = () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
        this.mediaRecorder?.stream.getTracks().forEach(track => track.stop());
        this.mediaRecorder = null;
        resolve(audioBlob);
      };

      this.mediaRecorder.stop();
      this.isRecording.set(false);
      clearInterval(this.durationInterval);
    });
  }

  canRecord(): boolean {
    return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
  }

}
