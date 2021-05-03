import Head from "next/head"
import { useRouter } from "next/router"
import { BingoGame } from "../../components/game/BingoGame"

export default function Game() {
  const router = useRouter()
  const gameCode = router.query.code as string || ""
  const playerName = router.query.name as string || undefined

  return (
    <>
      <Head>
        <title>MultiBingo : {gameCode}</title>
      </Head>
      <BingoGame gameCode={gameCode} playerName={playerName}/>
    </>
  )
}
