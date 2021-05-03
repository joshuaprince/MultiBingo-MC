import {
  Button,
  ButtonGroup,
  Input,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  useDisclosure
} from "@chakra-ui/react"
import { useRouter } from "next/router"
import React from "react"

import styles from "styles/Game.module.scss"

type IProps = {
  gameCode: string
}

type IState = {
  name: string
}

export const PlayerNameInput: React.FC<IProps> = (props) => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [ state, setState ] = React.useState<IState>({name: ""})
  const router = useRouter()

  const doSubmit = () => {
    const playerName = state.name;
    // window.location.search = new URLSearchParams({"name": playerName}).toString();
    router.push("/game/" + props.gameCode + "?" + new URLSearchParams({"name": playerName}).toString())
  }

  return (
    <>
      <Button
        size="lg"
        className={styles.playerNameInput}
        onClick={onOpen}
        m={8}
      >
        Join Game
      </Button>
      <Modal size="xs" isOpen={isOpen} onClose={onClose}>
        <ModalOverlay/>
        <ModalContent>
          <ModalCloseButton/>
          <ModalHeader>Join Game {props.gameCode}</ModalHeader>

          <ModalBody className={styles.body}>
            <Input
              value={state.name}
              onChange={e => setState(s => ({...s, name: e.target.value}))}
              placeholder="Enter your name"
            />
          </ModalBody>

          <ModalFooter>
            <ButtonGroup>
              <Button colorScheme="blue" onClick={doSubmit}>Join</Button>
              <Button onClick={onClose}>Close</Button>
            </ButtonGroup>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  )
}
