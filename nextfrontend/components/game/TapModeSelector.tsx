import React from "react"
import { FormLabel, Switch } from "@chakra-ui/react"

import { ResponsiveContext } from "./ResponsiveContext"

import styles from "styles/Game.module.scss"

export const TapModeSelector: React.FC = () => {
  const { tapToMark, setTapToMark } = React.useContext(ResponsiveContext)
  return (
    <div className={styles.tapModeSelector}>
      <FormLabel htmlFor="tap-mode-selector">
        Tap to Strategize
      </FormLabel>
      <Switch
        id="tap-mode-selector"
        checked={tapToMark}
        onChange={e => setTapToMark(e.target.checked)}
      />
      <FormLabel htmlFor="tap-mode-selector">
        Tap to Complete
      </FormLabel>
    </div>
  )
}
