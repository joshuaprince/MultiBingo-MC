import { ChakraProvider, extendTheme } from "@chakra-ui/react"
import type { AppProps } from "next/app"
import Head from "next/head"

import "styles/globals.css"

import "tippy.js/dist/tippy.css"
import { Layout } from "../components/Layout"


const theme = extendTheme({
  fonts: {
    // body: "Minecraftia.ttf"
  }
})

export default function BingoApp({ Component, pageProps }: AppProps) {
  return (
    <ChakraProvider theme={theme}>
      <Head>
        <title>MultiBingo</title>
        <meta name="description" content="Multi-player, multi-game Bingo" />
        <link rel="icon" href="/favicon.png" />
      </Head>
      <Layout>
        <Component {...pageProps} />
      </Layout>
    </ChakraProvider>
  )
}
