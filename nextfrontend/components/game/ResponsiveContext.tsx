import React from "react"

type IResponsiveContext = {
  /** If true, the @media used to render this app does not support hovering. */
  isTapOnly: boolean

  /** If true, taps on the board should mark the space tapped.
   * If false, taps on the board should covert-mark the space tapped. */
  tapToMark: boolean
  setTapToMark: (val: boolean) => void
}

export const ResponsiveContext = React.createContext<IResponsiveContext>({
  isTapOnly: false,
  tapToMark: true,
  setTapToMark: () => {}
})
