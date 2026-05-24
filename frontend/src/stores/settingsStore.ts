import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Settings, AsrProvider, LlmProvider } from '@/types/settings'

interface SettingsState {
  settings: Settings
  setAsrProvider: (provider: AsrProvider) => void
  setLlmProvider: (provider: LlmProvider) => void
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      settings: {
        asrProvider: 'whisper_local',
        llmProvider: 'claude',
      },

      setAsrProvider: (provider) =>
        set((s) => ({ settings: { ...s.settings, asrProvider: provider } })),

      setLlmProvider: (provider) =>
        set((s) => ({ settings: { ...s.settings, llmProvider: provider } })),
    }),
    {
      name: 'meetingsum-settings',
    },
  ),
)
