import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Settings, AsrProvider, LlmProvider } from '@/types/settings'

interface SettingsState {
  settings: Settings
  setAsrProvider: (provider: AsrProvider) => void
  setLlmProvider: (provider: LlmProvider) => void
  setSummaryTemplate: (template: string) => void
  resetTemplate: () => void
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      settings: {
        asrProvider: 'whisper_local',
        llmProvider: 'claude',
        summaryTemplate: '',
      },

      setAsrProvider: (provider) =>
        set((s) => ({ settings: { ...s.settings, asrProvider: provider } })),

      setLlmProvider: (provider) =>
        set((s) => ({ settings: { ...s.settings, llmProvider: provider } })),

      setSummaryTemplate: (template) =>
        set((s) => ({ settings: { ...s.settings, summaryTemplate: template } })),

      resetTemplate: () =>
        set((s) => ({ settings: { ...s.settings, summaryTemplate: '' } })),
    }),
    {
      name: 'meetingsum-settings',
    },
  ),
)
