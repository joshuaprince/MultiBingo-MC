import React from "react"
import NextLink from "next/link"
import { useRouter } from "next/router"
import Tippy from "@tippyjs/react"

import styles from "../styles/Layout.module.scss"

export const Navbar: React.FC = () => {
  const router = useRouter()
  const gameCode = router.query.code as string || undefined

  return (
    <nav className={styles.navbar}>
      <span>
        <NextLink href="/"><a className={styles.logo}>MultiBingo</a></NextLink>
        <NextLink href="/new"><a className={styles.link}>New Game</a></NextLink>
        <NextLink href="/about"><a className={styles.link}>About</a></NextLink>
      </span>

      <span>
        {gameCode &&
          <Tippy content={"Game Code"}>
            <span className={styles.gameCode}>{gameCode}</span>
          </Tippy>
        }
      </span>
    </nav>
  )
}
